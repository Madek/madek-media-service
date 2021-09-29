require 'requests/shared/authorization_error'

describe "Resources" do
  describe "Stores: /media-service/stores/", type: :request do
    let(:request) { faraday_client_with_token.get("stores/") }
    let(:response) { request }

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
      let(:api_token) { create(:api_token, user: user) }
      let(:user_token) { api_token.token_hash }
      let!(:store_1) do
        create(:media_store,
                :database,
                :with_users,
                users: 3.times.map { create(:user) })
      end
      let!(:store_2) { create(:media_store, :with_groups, groups: [create(:group)]) }
      let!(:store_3) do
        create(:media_store,
              :with_groups,
              :with_users,
              groups: 2.times.map { create(:group) },
              users: [user])
      end
      let(:response_body) { JSON.parse(response.body) }

      it "returns stores collection" do
        expect(response.status).to eq(200)
        expect(response_body).to be_a(Hash)


        expect(response_body).to have_key("stores")
        stores = response_body.fetch("stores")
        expect(stores).to be_an(Array)
        expect(stores.size).to eq(3)

        check_store_from_response(response_body, store_1, {
          groups_count: 0,
          users_count: 3,
          uploaders_count: 3
        })

        check_store_from_response(response_body, store_2, {
          groups_count: 1,
          users_count: 0,
          uploaders_count: nil
        })

        check_store_from_response(response_body, store_3, {
          groups_count: 2,
          users_count: 1,
          uploaders_count: 1
        })
      end
    end
  end
end

def check_store_from_response(response_body, store, **attrs_with_values)
  store_from_response = response_body.fetch("stores").detect { |s| s.fetch("id") == store.id }
  expect(store_from_response).to be

  expect(store_from_response["type"]).to eq(store.type)
  expect(store_from_response["description"]).to eq(store.description)
  expect(store_from_response["configuration"]).to be_nil
  attrs_with_values.each_pair do |attr, value|
    expect(store_from_response[attr.to_s]).to eq(value)
  end
end
