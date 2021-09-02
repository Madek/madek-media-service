require 'requests/shared/authorization_error'

describe "Resources" do
  describe "Settings: /media-service/settings/", type: :request do
    let(:request) { faraday_client_with_token.get("settings/") }
    let(:response) { request }
    let(:user) { create(:user, :with_system_admin_role) }

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
      let!(:settings) { create(:media_service_setting) }
      let(:api_token) { create(:api_token, user: user, scope_write: true) }
      let(:user_token) { api_token.token_hash }
      let(:parsed_body) { JSON.parse(response.body) }

      describe "getting settings" do
        it "responses with success" do
          expect(response.status).to eq(200)
        end

        it "returns settings" do
          expect(without_timestamps(parsed_body)).to eq({
            id: 0,
            private_key: nil,
            upload_min_part_size: 1024 ** 2,
            upload_max_part_size: 100 * 1024 ** 2,
          }.deep_stringify_keys)
        end
      end

      describe "updating" do
        let(:params) do
          {
            id: 0,
            private_key: "PRIVATE_KEY",
            upload_min_part_size: 10 * 1024 ** 2,
            upload_max_part_size: 12 * 1024 ** 2
          }
        end
        let(:request) { faraday_client_with_token.patch("settings/", params) }

        it "responds with success" do
          expect(response.status).to eq(200)
        end

        it "updates the settings" do
          expect(without_timestamps(parsed_body))
            .to eq(params.stringify_keys)
        end

        it "responds with updated settings" do
          expect(without_timestamps(parsed_body))
            .to eq(params.stringify_keys)
        end
      end
    end
  end
end
