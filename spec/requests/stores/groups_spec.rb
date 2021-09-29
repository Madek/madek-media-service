require 'requests/shared/authorization_error'

describe "Resources" do
  describe "Groups: /media-service/stores/:store_id/groups/", type: :request do
    let(:api_token) { create(:api_token, user: user, scope_write: true) }
    let(:user_token) { api_token.token_hash }
    let(:request) { faraday_client_with_token.get("stores/#{store_id}/groups/") }
    let(:response) { request }
    let(:store) { create(:media_store) }
    let(:store_id) { store.id }

    context "with public access" do
      let(:user) { nil }

      it_raises "authorization error"
    end

    context "for an ordinary user" do
      let(:user) { create(:user) }

      it_raises "authorization error"
    end

    context "for an user with admin role" do
      let(:user) { create(:user, :with_admin_role) }

      it_raises "authorization error"
    end

    context "for user with system admin role" do
      let!(:user) { create(:user, :with_system_admin_role) }
      let!(:group_1) { create(:group, name: "Empty group") }
      let!(:group_2) { create(:group, :with_user, name: "Masters") }
      let!(:group_3) { create(:group, name: "Family") }
      let!(:institutional_group) do
        create(:institutional_group,
               name: "Foo Institutional Group",
               institutional_name: "FOO.InstitutionalGroup")
      end

      before do
        group_3.users << create_list(:user, 3)
        store.groups << [group_1, group_2]

        store.media_store_groups.find_by(group: group_1).update_attribute(:priority, 2)
        store.media_store_groups.find_by(group: group_2).update_attribute(:priority, 5)
      end

      describe "listing" do
        let(:parsed_body) { JSON.parse(response.body) }

        it "responses with success" do
          expect(response.status).to eq(200)
        end

        it "returns users for the store" do
          expect(parsed_body).to eq({
            groups: [
              {
                name: group_1.name,
                group_id: group_1.id,
                institutional_name: nil,
                users_count: 0,
                priority: 2,
                key: group_1.id,
                index: 0,
                "page-index": 0
              }, {
                name: group_3.name,
                group_id: group_3.id,
                institutional_name: nil,
                users_count: 3,
                priority: nil,
                key: group_3.id,
                index: 1,
                "page-index": 1
              }, {
                name: institutional_group.name,
                group_id: institutional_group.id,
                institutional_name: institutional_group.institutional_name,
                users_count: 0,
                priority: nil,
                key: institutional_group.id,
                index: 2,
                "page-index": 2
              }, {
                name: group_2.name,
                group_id: group_2.id,
                institutional_name: nil,
                users_count: 1,
                priority: 5,
                key: group_2.id,
                index: 3,
                "page-index": 3
              }
            ]
          }.deep_stringify_keys)
        end
      end

      describe "filtering by user" do
        let(:response) do
          faraday_client_with_token
            .get("stores/#{store_id}/groups/?including-user=#{user_id}")
        end
        let(:searchable_user) { create(:user) }
        let(:user_id) { searchable_user.id }
        let(:parsed_body) { JSON.parse(response.body) }

        context "when user belongs to the group" do
          before { group_1.users << searchable_user }

          specify "group containing the user is returned" do
            expect(parsed_body).to eq(
              {
                groups: [
                  {
                    name: group_1.name,
                    group_id: group_1.id,
                    institutional_name: nil,
                    users_count: 1,
                    priority: 2,
                    key: group_1.id,
                    index: 0,
                    "page-index": 0
                  }
                ]
              }.deep_stringify_keys
            )
          end
        end

        context "when user does not belong to any of the groups" do
          specify "empty collection is returned" do
            expect(parsed_body).to eq(
              {
                groups: []
              }.deep_stringify_keys
            )
          end
        end
      end

      describe "updating group's priority" do
        let(:perform_request) do
          faraday_client_with_token
            .put("stores/#{store_id}/groups/#{group_1.id}/priority",
                priority: 3)
        end

        it "changes group's priority" do
          expect { perform_request }
            .to change { store.media_store_groups.find_by(group: group_1).priority }
            .from(2).to(3)
        end
      end

      describe "deleting group's priority" do
        let(:perform_request) do
          faraday_client_with_token
            .delete("stores/#{store_id}/groups/#{group_2.id}/priority")
        end

        it "responds with 204" do
          expect(perform_request.status).to eq(204)
        end

        it "deletes group's priority" do
          expect { perform_request }
            .to change { store.media_store_groups.where(group: group_2).count }
            .from(1).to(0)
        end
      end
    end
  end
end
