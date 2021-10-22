require "requests/uploads/shared/configuration"
require "requests/shared/authorization_error"

describe "Uploads", type: :request do
  include_context "configuration"

  describe "POST: uploads/:upload_id/complete" do
    let(:response) { complete_request }
    let(:store) { create(:media_store, :database, :with_users, users: [user].compact) }

    shared_examples "successful response" do
      let(:parsed_body) { JSON.parse(response.body) }

      context "when upload has 'started' state" do
        before do
          faraday_client_with_token.post("uploads/#{upload_id}/start")
        end

        it "responds with success" do
          expect(response.status).to eq(200)
        end

        it "returns upload details with 'completed' state" do
          expect(without_timestamps(parsed_body)).to eq(
            {
              id: upload_id,
              md5: md5,
              state: "completed",
              media_store_id: "database",
              size: file_size,
              uploader_id: user.id,
              content_type: "text/plain",
              filename: "small.txt",
              media_file_id: nil,
              error: nil,
              sha256: nil
            }.stringify_keys
          )
        end
      end

      context "when upload has 'announced' state" do
        let(:response) { complete_request }

        it "responds with 422 Unprocessable Entity status" do
          expect(response.status).to eq(422)
        end

        it "responds with error message" do
          expect(response.body).to include("expected upload state to be started but is announced")
        end
      end
    end

    context "with public access" do
      let(:user) { nil }
      let(:upload_id) { :not_relevant }

      it_raises "authorization error"
    end

    context "for an ordinary user" do
      let(:user) { create(:user) }

      include_examples "successful response"
    end

    context "for an user with admin role" do
      let(:user) { create(:user, :with_admin_role) }

      include_examples "successful response"
    end

    context "for user with system admin role" do
      let(:user) { create(:user, :with_system_admin_role) }

      include_examples "successful response"
    end
  end
end
